import { SchemaOptions } from 'mongoose';

export const stellaSchemaOptions: SchemaOptions = {
  timestamps: false,
  versionKey: false,
  toJSON: {
    virtuals: true,
    transform(_doc, ret: Record<string, unknown>) {
      ret.id = ret._id;
      delete ret._id;
      return ret;
    },
  },
  toObject: {
    virtuals: true,
    transform(_doc, ret: Record<string, unknown>) {
      ret.id = ret._id;
      delete ret._id;
      return ret;
    },
  },
};
