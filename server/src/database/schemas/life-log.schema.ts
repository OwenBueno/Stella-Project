import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument, Schema as MongooseSchema } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type LifeLogDocument = HydratedDocument<LifeLog>;

@Schema({ ...stellaSchemaOptions, collection: 'LifeLog' })
export class LifeLog {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true })
  type!: string;

  @Prop({ type: MongooseSchema.Types.Mixed, required: true })
  payload!: Record<string, unknown>;

  @Prop({ type: Date, required: true })
  timestamp!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;
}

export const LifeLogSchema = SchemaFactory.createForClass(LifeLog);
LifeLogSchema.index({ updatedAt: 1 });
